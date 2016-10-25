#pragma once

#include "declarations.h"

void sub_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs - rhs)
}

void sub_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs - rhs)
}
