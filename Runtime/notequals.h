#pragma once

#include "declarations.h"

void neq_b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs != rhs)
}

void neq_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs != rhs)
}

void neq_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs != rhs)
}
