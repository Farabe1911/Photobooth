# PhotoBooth Project

## Overview
The PhotoBooth project is a JavaFX application that allows users to capture images using a webcam, annotate them with event details, and print them. The application provides a user-friendly interface for selecting a webcam, capturing multiple photos, and choosing a template for printing.

## Features
- Select from available webcams
- Capture up to four photos with a countdown timer
- Annotate photos with custom event text and color
- Choose from multiple templates for printing
- Save captured images to a local directory
- Print images directly from the application

## Requirements
- Java Development Kit (JDK) 21 (matching the `pom.xml` configuration)
- Maven (for building and running the app)
- A compatible webcam
- Windows 10/11 (for the provided batch file launcher)

## How to Run the App

You can run the PhotoBooth in two main ways: with the included batch file (easiest on Windows) or directly from the command line without any extra files.

### Option 1 – Double-click (Windows, recommended)

1. Make sure Java 21 and Maven are installed and available on your `PATH`.
2. In File Explorer, open the project folder (the one containing `pom.xml`).
3. Double-click `Run-PhotoBooth.bat`.
4. A terminal window will open, run `mvn javafx:run`, and then launch the PhotoBooth window.  
   - If you see “‘mvn’ is not recognized…”, Maven is not on your `PATH`. Either install Maven or use Option 2 below.

### Option 2 – Run from the command line (without the batch file)

1. Open a terminal (Command Prompt or PowerShell).
2. Change directory to the project root (the folder with `pom.xml`). For example:

   ```powershell
   cd "C:\path\to\photobooth-1"
   ```

3. Run the JavaFX app via Maven:

   ```powershell
   mvn javafx:run
   ```

   Maven will download dependencies (first run only), compile the code, and start the PhotoBooth JavaFX window.

### Option 3 – Build a JAR and run it

If you prefer, you can build a “fat” JAR and run it directly with `java`:

1. From the project root, build the shaded JAR:

   ```powershell
   mvn -DskipTests clean package
   ```

2. Run the shaded JAR:

   ```powershell
   java -jar target\photobooth-1.0-SNAPSHOT-shaded.jar
   ```

### Running from VS Code / IDE (optional)

- If you run the `PhotoBooth` class directly from an IDE or the plain Java launcher (for example, VS Code’s Run button), you may see:

  `Error: JavaFX runtime components are missing, and are required to run this application`

  This happens because the Java launcher must be given JavaFX on the module path or classpath. The simplest way to avoid this is to run via Maven (Options 1 or 2 above), which wires JavaFX automatically.

- If you still want to run directly from an IDE, configure the run configuration to include the JavaFX modules. Example VM arguments (replace `PATH_TO_FX` with your JavaFX SDK `lib` path):

  ```text
  --module-path "PATH_TO_FX" --add-modules=javafx.controls,javafx.fxml,javafx.swing
  ```

## Usage
- Launch the application and select a webcam from the dropdown menu.
- Enter the event name in the provided text field.
- Choose the desired font size and color for the annotation.
- Click the "Capture 4 Photos" button to start capturing images.
- After capturing, select a template for printing and follow the prompts to save and print your images.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.
