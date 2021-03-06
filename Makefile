#	ANTLR4 must be set as an exported variable 
#		
#			export ANTLR4="/<PATH TO ANTLR JAR>/antlr-4.5-complete.jar"
#			export CLASSPATH="/<PATH TO ANTLR JAR>/antlr-4.5-complete.jar:$CLASSPATH"
#
## Comands:
# all:					Builds everything to make a runable program
# run:					Runs The program builds if necessary
# clean: 				Cleans generated files
# submissible:	Builds a submission file
#
## Variables (DON'T TOUCH THESE. THEY WILL ONLY CHANGE ON YOUR COPY)
#	SRC:			The source folder
#	GEN:			The folder where generated antlr files will go
#	BIN:			The folder where the generated java class will go
#	JAR:			The name of the jar execuable will go
#	SUBFILES: The files for submisssion
#	INTELLIJ:	Files from INTELLIJ
SRC=src
GEN=gen
BIN=bin
TMP=tmp
#LIB=lib
JAR=gazprea.jar
#RUNTIME=runtime
#RUNTIMEF=lib$(RUNTIME).a
TESTER=test_script
TESTFILES=TestFiles
#SUBFILES=$(SRC) $(LIB)/*.c $(LIB)/*.h $(TESTFILES) README
SUBFILES=$(SRC) $(TESTFILES) README
INTELLIJ=out
#dirty=$(MAKE) -q -C $(LIB) clean
src_files=$(shell find .$(src)/ -name '*.java')
all: $(JAR)

run: $(JAR)
ifeq "$f" ""
ifneq ("$(wildcard Input)","")
	@java -cp "$(JAR):$(ANTLR4)" Main Input > tmp.ll
else
	$(error "Usage: make run f=\"<filename>\"")
endif
else
	@java -cp "$(JAR):$(ANTLR4)" Main $f > tmp.ll
endif
	@clang++-3.6 -L$(CURDIR) -std=c11 -g -lm -s -o tmp tmp.ll #-l$(RUNTIME)
	@./tmp > out.tmp
	@cat out.tmp

test: $(JAR)
	./$(TESTER) -a -s

test_verbos: $(JAR)
	./$(tester) -a

compile: $(JAR)
ifeq "$f" ""
ifneq ("$(wildcard Input)","")
	@java -cp "$(JAR):$(ANTLR4)" Main Input > tmp.ll
else
	$(error "Usage: make run f=\"<filename>\"")
endif
else
	@java -cp "$(JAR):$(ANTLR4)" Main $f > tmp.ll
endif
	@clang++-3.6 -L$(CURDIR) -std=c11 -g -lm -s -o $(o) tmp.ll #-l$(RUNTIME)
	
clean:
ifneq "$(wildcard $(GEN))" ""
	@printf "Removing Generated files...\t"
	@-rm -rf $(GEN)
	@printf "Done\n"
endif
ifneq "$(wildcard $(BIN))" ""
	@printf "Removing Class files...\t\t"
	@-rm -rf $(BIN)
	@printf "Done\n"
endif
ifneq "$(wildcard $(JAR))" ""
	@printf "Removing Jar file...\t\t"
	@-rm -rf $(JAR) 
	@printf "Done\n"
endif
ifneq "$(wildcard submission_*.tar.gz)" ""
	@printf "Removing submission file...\t"
	@-rm -rf submission_*.tar.gz
	@printf "Done\n"
endif
ifneq "$(wildcard $(INTELLIJ))" ""
	@printf "Removing Intellij generated...\t"
	@-rm -rf $(INTELLIJ)
	@printf "Done\n"
endif
ifneq "$(wildcard $(TMP)*)" ""
	@printf "Removing temp file...\t\t"
	@-rm -rf $(TMP)* out.tmp
	@printf "Done\n"
endif
#	@$(MAKE) -s -C $(LIB) clean

submissible: clean
ifneq "$(wildcard submission_*.tar.gz)" ""
	@printf "Removing submission file...\t"
	@-rm -rf submission_*.tar.gz
	@printf "Done\n"
endif
	@printf "This will create a submissible tar ball for you to submit\n"
	@printf "Enter your student CCID (the one you use to logon to beartracks).\n"
	@printf "Then hit [enter]: "
	@read -r -p "" NAME; \
	tar -zcf submission_$$NAME.tar.gz $(SUBFILES);
	@printf "\nSubmissible ready\n"

$(JAR): $(GEN) $(BIN) #$(RUNTIMEF)
	@printf "Building Jar file..."
	@jar cfe $(JAR) Main -C $(BIN)/ .
	@printf "\t\tDone\n"

$(GEN): $(SRC)/*.g4
	@printf "Generating Antlr4 files..."
	@java -cp "$(ANTLR4)" org.antlr.v4.Tool $(SRC)/*.g4 -o $(GEN)/ \
	-listener -visitor
	@mv $(GEN)/src/* $(GEN)/
	@rmdir $(GEN)/src/
	@touch $(GEN)
	@printf "\tDone\n"

$(BIN): $(GEN) $(src_files)
	@printf "Building class files..."
	@mkdir -p $(BIN)
	@javac -cp "$(ANTLR4)" -d "$(BIN)" $(src_files)
	@touch $(BIN)
	@printf "\t\tDone\n"

#$(RUNTIMEF): $(shell find ./$(LIB)/ -name '*.c')
#	@printf "Building runtime library...\t"
#	@$(MAKE) -C $(LIB) -s
#	@printf "Done\n"
