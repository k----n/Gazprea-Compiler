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


void getAt2() {
    Value* value = stack->pop(); // this is the tuple


    	if (!(value)->isTuple()) {
    		printf("NOT A VECTOR OR TUPLE\n");
    		exit(1);
    	}

    	int ind =  value->tupleValue()->getCount();

    	    for (int i = 0; i < ind; i++) {
                Value *sVal = value->tupleValue()->get(i);
                stack->push(sVal);
                _unwrap();
                Value * tmp = stack->pop()->copy();
                stack -> push(tmp);
                        if (stack->peek()->isInteger()){
                            printf("\n%d\n", *(stack->peek()->integerValue()));
                        }
            }
}