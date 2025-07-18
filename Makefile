QUORUM ?= 3
SPECS ?= 1
MALFUNCTION ?= 0
DF_MAX_RESULT := $(shell expr $(QUORUM) + 5)
PATH_PROJECT_JAR = target/specialist_delegation-0.0.1-SNAPSHOT.jar
PROJECT_GROUP    = specialist_delegation
JADE_AGENTS      = creator:$(PROJECT_GROUP).Creator($(QUORUM), $(SPECS), $(MALFUNCTION));
JADE_FLAGS 		 = -gui -jade_domain_df_maxresult $(DF_MAX_RESULT) -agents "$(JADE_AGENTS)"

.PHONY:
	clean
	build-and-run

build-and-run:
	@echo "Building and executing the project..."
	make build run

build:
	@echo "Building the project"
	mvn clean install

run:
	@echo "Executing the project with the last build"
	java -cp $(PATH_PROJECT_JAR) jade.Boot $(JADE_FLAGS)

clean:
	@echo "Removing the existing build and auto-generated files"
	mvn clean
	rm -f APDescription.txt; rm -f MTPs-Main-Container.txt

help:
	@echo "This program has the following commands to execute:"
	@echo "	$$ make build "
	@echo "	  It's responsible for Building the Jar of this Building Block"
	@echo "	$$ make run"
	@echo "	  It's responsible for executing the program with graphic interface of JADE"
	@echo "	$$ make clean"
	@echo "	  It's responsible for cleaning the generated files"
	@echo "	$$ make build-and-run"
	@echo "	  Executes both commands build and run in this order"
	@echo "	$$ make help"
	@echo "	  Shows this help resume"
	@echo ""
	@echo "If wanted it's possible to change the quantity of agents by adding the variable QUORUM to the command, as seen in the next line"
	@echo "	$$ make build-and-run QUORUM=<Quantity of agents>"
	@echo "It's also possible to change the number of specialities of each agent by adding the variable SPECS to the command, as seen in the next line"
	@echo "	$$ make build-and-run SPECS=<Quantity of specialities>"
	@echo "It's possible to activate a random agent malfunction to test timeout more effectively, as seen in the next line"
	@echo "	$$ make build-and-run MALFUNCTION=1"