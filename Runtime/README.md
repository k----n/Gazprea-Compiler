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
 - Replace all `<` with `\<`
 - Replace all lines containing `REPLACE_ME-` with one of
   - `<variables :{ variable | <variable><\n>}>`
   - `<functions :{ function | <function><\n>}>`
   - `<code :{ line | <line><\n>}>`
   - `call void @GazFunc_main()`
 - Copy paste it in `runtime.stg replacing all but the first line`
 - Add `>>` at the bottom of `runtime.stg`
