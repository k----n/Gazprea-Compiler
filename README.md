# Gazprea

## Compile runtime

 - `cd Gazprea/Runtime/`
 - `clang -DLLVM_BUILD -DLLVM_NULLPTR -S -emit-llvm main.cpp`
 - Replace all `<` with `\<`
 - Replace all lines containing `REPLACE_ME-` with one of
   - `<variables :{ variable | <variable><\n>}>`
   - `<functions :{ function | <function><\n>}>`
   - `<code :{ line | <line><\n>}>`
   - `call void @GazFunc_main()`
 - Copy paste it in `runtime.stg replacing all but the first line`
 - Add `>>` at the bottom of `runtime.stg`

## Running tests

 - `cd Gazprea`
 - `make llvm f=./TestFiles/Input/<file>`

## Checking generated code

See `program.s`
