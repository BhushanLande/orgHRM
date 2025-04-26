{
  "tools": {
    "jdk": {
      "flavor": "azul",
      "version": "11"
    },
    "maven": "3.9.4"
  },
  "triggers": {
    "cron": {
      "spec": "30 02 * * *",
      "call": "automation"
    },
    "push": "disable",
    "pr": "disable",
    "manual": {
      "name": "build",
      "call": "default"
    }
  },
  "flows": {
    "default": [
      { "exposeVars": "maven" },
      { "call": "versionCheck" },
      { "call": "build" },
      {
        "mail": {
          "from": "bhushanlande525@gmail.com",
          "to": "bhushanlande525@gmail.com",
          "subject": "Orange HRM",
          "message": "build url: $BUILD_URL\n test report:"
        }
      }
    ],
    "automation": [
      { "exposeVars": "maven" },
      { "call": "versionCheck" },
      { "call": "buildAutomation" },
      {
        "mail": {
          "from": "bhushanlande525@gmail.com",
          "to": "bhushanlande525@gmail.com",
          "subject": "Orange HRM",
          "message": "build url: $BUILD_URL\n test report:"
        }
      }
    ],
    "pr": [
      {
        "try": [
          { "echo": "Running build for $GITHUB_PR_URL" },
          { "call": "versionCheck" },
          { "name": "Maven build", "command": "mvn -B -U clean verify -Dmaven.test.skip" },
          { "name": "Maven install", "command": "mvn clean deploy -Dmaven.test.skip" },
          { "name": "Run Test", "command": "mvn -f pom.xml clean verify" },
          { "name": "Run Test", "command": "mvn -f pom_cucumber.xml verify" },
          { "name": "Aggregate Report", "command": "mvn serenity:aggregate" }
        ],
        "catch": [
          { "echo": "Build failed" }
        ],
        "finally": [
          { "call": "generateReport" }
        ]
      }
    ],
    "versionCheck": [
      { "name": "JDK Version", "command": "java --version" },
      { "name": "Maven Version", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml -v" }
    ],
    "runNewTest": [
      {
        "try": [
          { "name": "Run Test Cucumber", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml verify -Pserenity-cucumber" }
        ],
        "catch": [
          { "echo": "Run Test Completed" }
        ]
      }
    ],
    "runTestAggregate": [
      {
        "try": [
          { "name": "Aggregate Report", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml verify -Pserenity-cucumber serenity:aggregate" }
        ],
        "catch": [
          { "echo": "Serenity Report aggregate task completed" }
        ]
      }
    ],
    "runTestAutomation": [
      {
        "try": [
          { "name": "Run Test Cucumber", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml verify -Pserenity-cucumber" }
        ],
        "catch": [
          { "echo": "Run Test Completed" }
        ]
      }
    ],
    "build": [
      {
        "try": [
          { "exposeVars": "maven" },
          { "name": "Project information", "command": "echo Building $(MAVEN_GROUP_ID):$(MAVEN_ARTIFACT_ID):$(MAVEN_VERSION)" },
          { "name": "Maven install", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml clean deploy -Dmaven.test.skip" },
          { "call": "runNewTest" },
          { "call": "runTestAggregate" },
          {
            "parallel": [
              { "call": "hygieiaPublish" }
            ]
          }
        ],
        "catch": [
          { "echo": "Build failed" }
        ],
        "finally": [
          { "call": "generateReport" }
        ]
      }
    ],
    "buildAutomation": [
      {
        "try": [
          { "exposeVars": "maven" },
          { "name": "Project information", "command": "echo Building $(MAVEN_GROUP_ID):$(MAVEN_ARTIFACT_ID):$(MAVEN_VERSION)" },
          { "name": "Maven install", "command": "mvn -s $(MAVEN_HOME)/conf/settings.xml clean deploy -Dmaven.skip.test" },
          { "call": "runTestAutomation" },
          { "call": "runTestAggregate" },
          {
            "parallel": [
              { "call": "hygieiaPublish" }
            ]
          }
        ],
        "catch": [
          { "echo": "Build failed" }
        ],
        "finally": [
          { "call": "generateReport" }
        ]
      }
    ],
    "hygieiaPublish": [
      { "hygieia.publishBuild": {} }
    ],
    "generateReport": [
      {
        "publishReport": {
          "context": "TestNG",
          "dir": "/target/site",
          "index": "serenity/index.html",
          "verbose": true
        }
      }
    ]
  }
}
