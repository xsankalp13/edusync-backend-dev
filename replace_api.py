import os
import glob

controller_dir = "/Users/sankalp/Desktop/Shiksha Intellegence/backend/src/main/java/com/project/edusync/finance/controller"

for file_path in glob.glob(os.path.join(controller_dir, "*.java")):
    with open(file_path, "r") as f:
        content = f.read()

    new_content = content.replace('"\/auth\/finance', '"${api.url}\/auth\/finance')
    new_content = new_content.replace('"/auth/finance', '"${api.url}/auth/finance')

    if content != new_content:
        with open(file_path, "w") as f:
            f.write(new_content)
        print(f"Updated {file_path}")

print("Done.")
