#pragma once 

#include "declarations.h"

void add_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs + rhs)
}

void add_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs + rhs)
}
