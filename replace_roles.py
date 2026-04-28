import os
import glob

controller_dir = "/Users/sankalp/Desktop/Shiksha Intellegence/backend/src/main/java/com/project/edusync/finance/controller"

for file_path in glob.glob(os.path.join(controller_dir, "*.java")):
    with open(file_path, "r") as f:
        content = f.read()

    # Avoid duplicate substitutions if script runs twice
    if "'ROLE_SCHOOL_ADMIN'" not in content and "'ROLE_ADMIN'" in content:
        new_content = content.replace("'ROLE_ADMIN'", "'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN'")
        
        with open(file_path, "w") as f:
            f.write(new_content)
        print(f"Updated roles in {file_path}")

print("Done replacing roles.")
