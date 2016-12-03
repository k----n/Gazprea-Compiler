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
    _unwrap();
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
    }
}

void indexVector() {
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if (!(value2)->isVector()){
        printf("Indexing type is not a vector\n");
        exit(1);
    }

    int vectorSize = value2->vectorValue()->getCount();

    if ((value1)->isVector()){
        int size = value1->vectorValue()->getCount();

        int index = 0;
        Value * node;
        Value * element;

        Vector<Value>* vectorValues = new Vector<Value>;

        for (int i = 0; i < size; i++){
            node = value1->vectorValue()->get(i);
            if (!(node)->isInteger()){
                printf("Index must be integer type\n");
                exit(1);
            }
            if (index > vectorSize){
                printf("Index out of range\n");
                exit(1);
            }
            index = *(node->integerValue());

            element = value2->vectorValue()->get(index - 1)->copy();

            vectorValues->append(element);

        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;
    }
    else if (value1->isInterval()){
        stack->push(value1);
        promoteTo_v();
        value1 = stack->pop(); // value1 is now a vector

        int size = value1->vectorValue()->getCount();

        int index = 0;
        Value * node;
        Value * element;

        Vector<Value>* vectorValues = new Vector<Value>;

        for (int i = 0; i < size; i++){
            node = value1->vectorValue()->get(i);
            if (!(node)->isInteger()){
                printf("Index must be integer type\n");
                exit(1);
            }
            if (index > vectorSize){
                printf("Index out of range\n");
                exit(1);
            }
            index = *(node->integerValue());

            element = value2->vectorValue()->get(index - 1)->copy();

            vectorValues->append(element);

        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;

    }
    else if (value1->isInteger()){
        int index = *(value1->integerValue());
        if (index > vectorSize){
            printf("Index out of range\n");
            exit(1);
        }

        Value* element = value2->vectorValue()->get(index - 1)->copy();
        stack -> push(element);
        return;
    }
    else {
        printf("Index must be integer type\n");
        exit(1);
    }
}