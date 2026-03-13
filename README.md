# LinkedInProject

## Introduction
LinkedInProject is a Java-based automated testing framework designed to interact with LinkedIn's web interface. It leverages Selenium WebDriver and TestNG to automate login, post interactions (like, comment, repost), and data extraction tasks. The project is structured for extensibility and maintainability, making it suitable for both regression and feature testing of LinkedIn workflows.

## Features
- Automated login to LinkedIn using encrypted credentials
- Automated testing for:
  - Liking posts
  - Commenting on posts
  - Reposting content
- Extraction and export of user interaction data (CSV/HTML)
- Configurable via properties file
- TestNG-based test suite for easy test management
- HTML and CSV reporting for test results

## Installation
1. **Clone the repository:**
   ```sh
   git clone <repository-url>
   cd LinkedInProject
   ```
2. **Install dependencies:**
   Ensure you have Java 21+ and Maven installed. Then run:
   ```sh
   mvn clean install
   ```

## Configuration
Edit `src/test/resources/config.properties` to set your LinkedIn credentials and application URL:
```ini
app.url=https://www.linkedin.com/login
username=YOUR_USERNAME
password=ENCRYPTED_PASSWORD
```
- The password should be AES-encrypted. Use the `RunOncePasswordGeneration` utility to generate an encrypted password:
  ```sh
  mvn exec:java -Dexec.mainClass="RunOncePasswordGeneration"
  ```

## Usage
- **Run all tests:**
  ```sh
  mvn test
  ```
- **Test suite configuration:**
  The main test suite is defined in `testng.xml` and includes:
  - `PostLikeTest`
  - `PostCommentsTest`
  - `PostRepostTest`

- **Test data:**
  Place your test data in `src/test/resources/testdata/` as needed.

## Testing
- Test results and reports are generated in `src/test/resources/testOutput/` and `target/surefire-reports/`.
- Reports include HTML and CSV summaries of user interactions.
- To view reports, open the generated HTML files in your browser.

## Project Structure
```
LinkedInProject/
├── pom.xml                  # Maven project file
├── testng.xml               # TestNG suite configuration
├── src/
│   ├── main/java/utils/     # Utility classes (CSV, HTML, encryption, etc.)
│   └── test/java/tests/     # Test classes (BaseTest, PostLikeTest, etc.)
│   └── test/resources/      # Config, test data, and output
│       ├── config.properties
│       ├── testdata/
│       └── testOutput/
└── target/                  # Build and test output
```

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request. For major changes, open an issue first to discuss your ideas.

## Contact
For questions or support, please contact the project maintainer.

