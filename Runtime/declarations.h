#pragma once

#include "Stack.h"
#include "Value.h"

#define LOAD(type, name, call) \
	_unwrap(); \
	Value* name##_ = stack->pop(); \
	type* name##_ptr = name##_->call(); \
	type name = *name##_ptr; \
	delete name##_ptr; \
	name##_->release();

#define VALUE_OP(op) \
	Value* newValue = new Value(op); \
	stack->push(newValue); \
	newValue->release(); \

void _unwrap();

extern Stack<Value>* stack;
