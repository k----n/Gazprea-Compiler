# Gazprea

## Compile runtime

 - `cd Gazprea/Runtime/`
 - `clang -DLLVM_BUILD -DLLVM_NULLPTR -S -emit-llvm main.cpp`
 - `cat main.ll | sed -e "s/</\\\\</g" | sed -e "/REPLACE_ME-GLOBAL_VARIABLES/c\<variables :{ variable | <variable><\\\\n>}>" | sed -e "/REPLACE_ME-FUNCTIONS/c\<functions :{ function | <function><\\\\n>}>" | sed -e "/REPLACE_ME-CALL_DEFINED_MAIN/c\  call void @GazFunc_main()" | sed -e "/REPLACE_ME-GLOBAL_INITS/c\<code :{ line | <line><\\\\n>}>" | sed -e "1s/^/runtime(variables, functions, code) ::= <<\n/" | sed -e "\$a>>" > ../src/runtime.stg`

## Running tests

 - `cd Gazprea`
 - `make llvm f=./TestFiles/Input/<file>`

## Checking generated code

See `program.s`
