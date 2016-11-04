#pragma once

void getAt(int index) {
	Value* value = stack->pop();
	if (!value->isLvalue()) {
		printf("Not an lvalue");
		exit(1);
	}
	Value** ptr = value->lvalue_ptr();
	if (!(*ptr)->isTuple()) {
		printf("NOT A VECTOR OR TUPLE\n");
		exit(1);
	}
	
	Value* lvalue = ((Vector<Value>*)(*ptr)->value_ptr())->getLvalue(index);
	stack->push(lvalue);
	lvalue->release();
}

void getAt2(int index) {
    Value *tuple = stack->peek();
    Vector<Value>* tupleValue = tuple->tupleValue();
    Value *sVal = tupleValue->get(index);
    stack->push(sVal);
}