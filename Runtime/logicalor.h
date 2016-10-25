#pragma once

#include "declarations.h"

void lor_b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs || rhs)
}
