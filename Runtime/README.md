# Gazprea Runtime

 - Structs are for internal use by a single class only.
 - Classes all inherit from `Object`, and only `retain()`, `release()`, and `copy()` should be used.
 - `new` created an instance with `ref_count = 1`
 - When `ref_count` reaches 0, `delete` is implicitly called
 - `copy()` creates an exact duplicate execpt for `ref_count = 1`

 - Functions that take pointers may call `retain()` and `release()`
 - Functions that return pointers will `retain()` and it is up to the receiver to `release()`

 - Pointers to raw values must be `delete`d manually

## Compile runtime

 - `cd Gazprea/Runtime/`
 - `clang -DLLVM_BUILD -DLLVM_NULLPTR -S -emit-llvm main.cpp`
 - `cat main.ll | sed -e "s/</\\\\</g" | sed -e "/REPLACE_ME-GLOBAL_VARIABLES/c\<variables :{ variable | <variable><\\\\n>}>" | sed -e "/REPLACE_ME-FUNCTIONS/c\<functions :{ function | <function><\\\\n>}>" | sed -e "/REPLACE_ME-CALL_DEFINED_MAIN/c\  call void @GazFunc_main()" | sed -e "/REPLACE_ME-GLOBAL_INITS/c\<code :{ line | <line><\\\\n>}>" | sed -e "/REPLACE_ME-STRUCTS/c\<structs :{ struct | <struct><\\\\n>}>" | sed -e "1s/^/runtime(variables, functions, code, structs) ::= <<\n/" | sed -e "\$a>>" > ../src/runtime.stg`
