#pragma once

#include "declarations.h"

void lxr_b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP((lhs ^ rhs) != 0)
}
