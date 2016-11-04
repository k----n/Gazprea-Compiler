#pragma once

#include "Vector.h"
#include "Value.h"

template <class T>
Value* Vector<T>::getLvalue(int index) const {
	if (index < 0) { printf("Index out of bounds\n"); exit(1); }
	if (index >= this->count) { printf("Index out of bounds\n"); exit(1); }
	
	VectorNode* node = this->firstNode;
	for (int i = 0; i < index; ++i) {
		node = node->next;
	}
	ValueType* type = new ValueType(Lvalue);
	Value* lvalue = new Value(type, &node->value);
	return lvalue;
}
