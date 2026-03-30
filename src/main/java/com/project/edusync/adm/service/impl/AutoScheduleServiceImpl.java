package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.model.entity.CurriculumMap;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.repository.TimeslotRepository;
import com.project.edusync.adm.repository.CurriculumMapRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.service.AutoScheduleService;
import com.project.edusync.adm.service.ScheduleService;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScheduleServiceImpl implements AutoScheduleService {

    private final SectionRepository sectionRepository;
    private final TimeslotRepository timeslotRepository;
    private final CurriculumMapRepository curriculumMapRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;
    private final TeacherDetailsRepository teacherDetailsRepository;
    private final ScheduleService scheduleService;
    private final TransactionTemplate transactionTemplate;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter generateTimetableStream(UUID sectionId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minute timeout
        
        executor.execute(() -> {
            try {
                transactionTemplate.execute(status -> {
                    try {
                        runGeneticAlgorithm(sectionId, emitter);
                    } catch (IOException e) {
                        throw new RuntimeException("SSE stream failed", e);
                    }
                    return null;
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Error in Genetic Algorithm for section: {}", sectionId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", e.getMessage() != null ? e.getMessage() : "Unknown algorithm error")));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public void processGeneration(UUID sectionId) {
        // Not implemented as a standalone method yet
    }

    private void runGeneticAlgorithm(UUID sectionId, SseEmitter emitter) throws IOException {
        // Signal connection is established
        emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Evolution engine initialized")));

        Section section = sectionRepository.findByUuid(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        
        List<Timeslot> timeslots = timeslotRepository.findAllActive().stream()
                .filter(ts -> !Boolean.TRUE.equals(ts.getIsBreak()) && !Boolean.TRUE.equals(ts.getIsNonTeachingSlot()))
                .collect(Collectors.toList());
        
        List<CurriculumMap> curriculumMaps = curriculumMapRepository.findActiveByClassUuid(section.getAcademicClass().getUuid());
        
        // Load data needed for constraints
        List<Schedule> otherSchedules = scheduleRepository.findAllActive().stream()
                .filter(s -> s.getSection() != null && !s.getSection().getUuid().equals(sectionId))
                .collect(Collectors.toList());
        
        List<Room> allRooms = roomRepository.findAllActive();
        
        // Initialize Population
        int populationSize = 50;
        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(Individual.random(section, timeslots, curriculumMaps, allRooms, teacherDetailsRepository));
        }

        int maxGenerations = 200;
        Individual bestSoFar = null;

        for (int gen = 1; gen <= maxGenerations; gen++) {
            // Evaluate Fitness
            for (Individual ind : population) {
                ind.calculateFitness(otherSchedules);
            }

            // Sort by fitness (Descending)
            population.sort(Comparator.comparingInt(Individual::getFitness).reversed());
            
            Individual currentBest = population.get(0);
            if (bestSoFar == null || currentBest.fitness > bestSoFar.fitness) {
                bestSoFar = currentBest;
                
                // Stream update to client
                Map<String, Object> update = new HashMap<>();
                update.put("generation", gen);
                update.put("fitness", currentBest.fitness);
                update.put("isComplete", currentBest.fitness >= 0); // 0 or positive is conflict-free
                update.put("schedule", convertToDtos(currentBest));
                
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(update));
            }

            if (bestSoFar.fitness >= 0) {
                log.info("Optimal solution found at generation {}", gen);
                break;
            }

            // Natural Selection & Reproduction
            List<Individual> nextGen = new ArrayList<>();
            // Elitism: Keep top 5
            nextGen.addAll(population.subList(0, 5));

            while (nextGen.size() < populationSize) {
                Individual parent1 = tournamentSelect(population);
                Individual parent2 = tournamentSelect(population);
                Individual offspring = parent1.crossover(parent2);
                offspring.mutate(timeslots, curriculumMaps, allRooms, teacherDetailsRepository);
                nextGen.add(offspring);
            }
            population = nextGen;
        }

        emitter.send(SseEmitter.event().name("complete").data("Generation finished"));
        emitter.complete();
    }

    private List<ScheduleResponseDto> convertToDtos(Individual individual) {
        // Simple mapping for demonstration - in real impl, use a mapper
        return individual.geneMap.values().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ScheduleResponseDto mapToDto(Gene gene) {
        // Mocking the rich DTO for the streamer
        return ScheduleResponseDto.builder()
                .timeslot(ScheduleResponseDto.NestedTimeslotResponseDto.builder()
                        .uuid(gene.timeslot.getUuid())
                        .startTime(gene.timeslot.getStartTime())
                        .endTime(gene.timeslot.getEndTime())
                        .dayOfWeek(gene.timeslot.getDayOfWeek())
                        .build())
                .subject(ScheduleResponseDto.NestedSubjectResponseDto.builder()
                        .uuid(gene.subject.getUuid())
                        .name(gene.subject.getName())
                        .build())
                .teacher(ScheduleResponseDto.NestedTeacherResponseDto.builder()
                        .id(gene.teacher.getStaff().getId())
                        .name(gene.teacher.getStaff().getUserProfile().getFirstName())
                        .build())
                .room(ScheduleResponseDto.NestedRoomResponseDto.builder()
                        .uuid(gene.room.getUuid())
                        .name(gene.room.getName())
                        .roomType(gene.room.getRoomType())
                        .totalCapacity(gene.room.getTotalCapacity())
                        .build())
                .build();
    }

    private Individual tournamentSelect(List<Individual> population) {
        Random rand = new Random();
        Individual best = null;
        for (int i = 0; i < 3; i++) {
            Individual ind = population.get(rand.nextInt(population.size()));
            if (best == null || ind.fitness > best.fitness) {
                best = ind;
            }
        }
        return best;
    }

    @Data
    @Builder
    private static class Individual {
        Map<UUID, Gene> geneMap; // timeslotId -> Gene
        int fitness;

        static Individual random(Section section, List<Timeslot> timeslots, List<CurriculumMap> curriculumMaps, List<Room> rooms, TeacherDetailsRepository teacherRepo) {
            Map<UUID, Gene> genes = new HashMap<>();
            Random rand = new Random();
            
            // For each curriculum requirement, pick a random slot
            for (CurriculumMap cm : curriculumMaps) {
                for (int p = 0; p < cm.getPeriodsPerWeek(); p++) {
                    Timeslot ts = timeslots.get(rand.nextInt(timeslots.size()));
                    // Avoid multiple assignments to same slot in one individual initially
                    if (genes.containsKey(ts.getUuid())) continue; 

                    List<TeacherDetails> availableTeachers = teacherRepo.findQualifiedTeachersForSubject(cm.getSubject().getUuid());
                    if (availableTeachers.isEmpty()) continue;
                    
                    TeacherDetails teacher = availableTeachers.get(rand.nextInt(availableTeachers.size()));
                    Room room = rooms.get(rand.nextInt(rooms.size()));

                    genes.put(ts.getUuid(), Gene.builder()
                            .timeslot(ts)
                            .subject(cm.getSubject())
                            .teacher(teacher)
                            .room(room)
                            .build());
                }
            }
            return Individual.builder().geneMap(genes).build();
        }

        void calculateFitness(List<Schedule> others) {
            int score = 0;
            Set<Long> teacherTimeslotSet = new HashSet<>();
            Set<UUID> roomTimeslotSet = new HashSet<>();

            // Rule 1: No internal conflicts
            // Handled by the Map structure mostly (one subject per slot), 
            // but we need to check if multiple genes accidentally share resources? No, genes are one per slot.

            // Rule 2: Global Teacher Conflict
            for (Gene gene : geneMap.values()) {
                if (gene.teacher == null || gene.timeslot == null) continue;
                
                // Check against others (Schedule entities from other sections)
                boolean conflict = others.stream().anyMatch(s -> 
                    s.getTeacher() != null && s.getTimeslot() != null &&
                    s.getTeacher().getId().equals(gene.teacher.getId()) && 
                    s.getTimeslot().getUuid().equals(gene.timeslot.getUuid())
                );
                
                if (conflict) score -= 1000;
            }

            // Rule 3: Global Room Conflict
            for (Gene gene : geneMap.values()) {
                if (gene.room == null || gene.timeslot == null) continue;

                boolean conflict = others.stream().anyMatch(s -> 
                    s.getRoom() != null && s.getTimeslot() != null &&
                    s.getRoom().getUuid().equals(gene.room.getUuid()) && 
                    s.getTimeslot().getUuid().equals(gene.timeslot.getUuid())
                );
                
                if (conflict) score -= 1000;
            }

            // Rule 4: Balance (Soft Constraint)
            // Penalty for multiple heavy subjects same day?
            
            this.fitness = score;
        }

        Individual crossover(Individual other) {
            Map<UUID, Gene> childGenes = new HashMap<>();
            Random rand = new Random();
            Set<UUID> allSlots = new HashSet<>();
            allSlots.addAll(this.geneMap.keySet());
            allSlots.addAll(other.geneMap.keySet());

            for (UUID slotId : allSlots) {
                if (rand.nextBoolean()) {
                    if (this.geneMap.containsKey(slotId)) childGenes.put(slotId, this.geneMap.get(slotId));
                } else {
                    if (other.geneMap.containsKey(slotId)) childGenes.put(slotId, other.geneMap.get(slotId));
                }
            }
            return Individual.builder().geneMap(childGenes).build();
        }

        void mutate(List<Timeslot> allTimeslots, List<CurriculumMap> curriculumMaps, List<Room> allRooms, TeacherDetailsRepository teacherRepo) {
            Random rand = new Random();
            if (rand.nextDouble() > 0.1) return; // 10% mutation rate

            if (geneMap.isEmpty()) return;
            
            // Pick a random gene to mutate
            UUID randomKey = new ArrayList<>(geneMap.keySet()).get(rand.nextInt(geneMap.size()));
            Gene gene = geneMap.get(randomKey);

            // Change room or teacher
            if (rand.nextBoolean()) {
                gene.room = allRooms.get(rand.nextInt(allRooms.size()));
            } else {
                List<TeacherDetails> available = teacherRepo.findQualifiedTeachersForSubject(gene.subject.getUuid());
                if (!available.isEmpty()) {
                    gene.teacher = available.get(rand.nextInt(available.size()));
                }
            }
        }
    }

    @Data
    @Builder
    private static class Gene {
        Timeslot timeslot;
        Subject subject;
        TeacherDetails teacher;
        Room room;
    }
}
