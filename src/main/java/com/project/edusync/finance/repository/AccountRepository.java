package com.project.edusync.finance.repository;

import com.project.edusync.finance.model.entity.Account;
import com.project.edusync.finance.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /** All root-level accounts (no parent) — the entry points for the COA tree. */
    List<Account> findByParentAccountIsNullAndSchoolIdOrderByCode(Long schoolId);

    /** All accounts of a given type for a school (e.g., all EXPENSE accounts). */
    List<Account> findByAccountTypeAndSchoolIdOrderByCode(AccountType accountType, Long schoolId);

    /** All active posting accounts visible for journal entry dropdowns. */
    List<Account> findByActiveAndPostingAccountAndSchoolIdOrderByCode(boolean active, boolean postingAccount, Long schoolId);

    /** Lookup by code within a school — used during COA seeding. */
    Optional<Account> findByCodeAndSchoolId(String code, Long schoolId);

    /** Find all direct children of a given parent account. */
    @Query("SELECT a FROM Account a WHERE a.parentAccount.id = :parentId ORDER BY a.code")
    List<Account> findChildrenOf(@Param("parentId") Long parentId);

    /** Check uniqueness of code before save. */
    boolean existsByCodeAndSchoolIdAndIdNot(String code, Long schoolId, Long excludeId);

    boolean existsByCodeAndSchoolId(String code, Long schoolId);

    /** Fetch all accounts for a school. */
    List<Account> findBySchoolId(Long schoolId);
}
