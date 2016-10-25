#pragma once

#include "declarations.h"

void mul_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs * rhs)
}

void mul_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs * rhs)
}
