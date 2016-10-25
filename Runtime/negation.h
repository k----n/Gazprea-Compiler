#pragma once

#include "declarations.h"

void neg_b() {
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(!lhs)
}

void neg_i() {
	LOAD(int, lhs, integerValue)
	VALUE_OP(-lhs)
}

void neg_r() {
	LOAD(float, lhs, realValue)
	VALUE_OP(-lhs)
}
