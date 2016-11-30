#pragma once

#include "declarations.h"

void lnd_b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs && rhs)
}

void lnd_v() {

}