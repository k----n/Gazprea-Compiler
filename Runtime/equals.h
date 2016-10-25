#pragma once

#include "declarations.h"

void eq__b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs == rhs)
}

void eq__i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs == rhs)
}

void eq__r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs == rhs)
}
