#pragma once

#include "Value.h"
#include "Stack.h"

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

bool toBool();

extern Stack<Value>* stack;
