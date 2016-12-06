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

void indexMatrix() {
    // left is the matrix
    // right is the row
    // column is the column
    // this is the way they appear on the stack
    Value* column = stack -> pop();
    Value* row = stack -> pop();
    Value* matrix = stack -> pop();

    // promote the intervals to vectors
    if (row->isInterval()){
        stack -> push(row);
        promoteTo_v();
        row = stack -> pop();
    }
    if (column->isInterval()){
        stack -> push(column);
        promoteTo_v();
        column = stack -> pop();
    }

    // first we need to get the row(s)
    if (row->isInteger()){
        Value* result = matrix->matrixValue()->get(*row->integerValue() - 1);
        // then we get the columns from the rows
        if (column->isInteger()){
            Value* indexed = result->vectorValue()->get(*column->integerValue() - 1);
            Vector<Value>* newVector = new Vector<Value>;
            newVector -> append(indexed);
            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, newVector);
            return;
        } else if (column->isVector()){
            int size = column->vectorValue()->getCount();
            Vector<Value>* newVector = new Vector<Value>;
            for (int i = 0; i < size; i++){
                newVector->append(result->vectorValue()->get(i));
            }
            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, newVector);
            return;
        } else {
            printf("Invalid index type\n");
            exit(1);
        }
    } else if (row->isVector()){
        Vector<Value>* rows = new Vector<Value>;
        int size = row->vectorValue()->getCount();
        for (int i = 0; i < size; i++){
            rows->append(matrix->matrixValue()->get(i));
        }
        int rowCount = rows->getCount();
        // then we get the columns from the rows
        if (column->isInteger()){
            Vector<Value>* newVector = new Vector<Value>;
            for (int i = 0; i < rowCount; i++){
                newVector->append(rows->get(i)->vectorValue()->get(*column->integerValue() - 1));
            }
            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, newVector);
            return;
        } else if (column->isVector()){
            // this result will be a matrix
            Vector<Value>* newMatrix = new Vector<Value>;
            int columnSize = column->vectorValue()->getCount();
            int columnCount = 0;
            int rowCount = 0;
            for (int i = 0; i < rowCount; i++){
                Vector<Value>* newVector = new Vector<Value>;
                rowCount+=1;
                for (int j = 0; j < columnSize; j++){
                    newVector->append(rows->get(i)->vectorValue()->get(j));
                    columnCount+=1;
                }
                ValueType* newType = new ValueType(VectorType);
                Value* newValue = new Value(newType, newVector);
                newMatrix->append(newValue);
            }
            ValueType* newType = new ValueType(MatrixType);
            newType->setVectorSize(columnCount);
            newType->setMatrixSize(rowCount);
            newType->setContainedType(rows->get(0)->vectorValue()->get(0)->getType()->getType());
            Value* newValue = new Value(newType, newMatrix);
        } else {
            printf("Invalid index type\n");
            exit(1);
        }
    } else {
        printf("Invalid index type\n");
        exit(1);
    }
}