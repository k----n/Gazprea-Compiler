#pragma once

#include "declarations.h"

void gt__i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs > rhs)
}

void gt__r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs > rhs)
}

void gt__() {
}