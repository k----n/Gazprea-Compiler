#pragma once

#include "declarations.h"

void mod_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs % rhs)
}

void mod_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(fmodf(lhs, rhs))
}
