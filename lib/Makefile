## (DON'T TOUCH THESE. THEY WILL ONLY CHANGE ON YOUR COPY)
# 
# Comands:
# all:					Builds everything to make a runable program
# clean: 				Cleans generated files
#
#
#
# These are the compile rules generated for each ".c" file
# Find all c files
CFILES = $(wildcard *.c)
LIBS = $(patsubst %.c, %, $(CFILES))
RUNTIME = ../libruntime.a

# This calls all the build commands
.PHONY: all
all: $(RUNTIME)

# Builds runtime static library
$(RUNTIME): $(foreach lib, $(LIBS), $(lib).o $(lib).ll) 
ifeq "$(CFILES)" ""
	$(warning No library files to link)
else
	@ar rcs $(RUNTIME) *.o
endif

%.o: %.c %.h
	@clang -std=c11 -g -c -o $@ $<

%.ll: %.c %.h
	@clang -g -emit-llvm -c -S -std=c11 $<

.PHONY: clean
clean:
ifneq "$(wildcard *.o)" ""
	@printf "Removing object files...\t"
	@rm *.o
	@printf "Done\n"
endif
ifneq "$(wildcard *.ll)" ""
	@printf "Removing llvm ir files...\t"
	@rm *.ll
	@printf "Done\n"
endif
ifneq "$(wildcard $(RUNTIME))" ""
	@printf "Removing runtime library file..."
	@rm $(RUNTIME) 
	@printf "Done\n"
endif
