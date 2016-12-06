#pragma once

#include "Object.h"
#include "BuiltinType.h"

class ValueType : public Object {
public:
	ValueType(BuiltinType type) : super(TypeValueType) {
		this->builtinType = type;
		this->has_vector_size = false;
		this->has_matrix_size = false;
		this->has_contained_type = false;
		this->vector_size = -1;
		this->matrix_size = -1;
		this->containedType = NullType;
	}

	ValueType(BuiltinType thisType, BuiltinType containedType, bool has_vector_size,
	    bool has_matrix_size, bool has_contained_type, int vector_size, int matrix_size) : super(TypeValueType) {
	    this->builtinType = thisType;
	    this->containedType = containedType;
	    this->has_vector_size = has_vector_size;
	    this->has_matrix_size = has_matrix_size;
	    this->has_contained_type = has_contained_type;
	    this->vector_size = vector_size;
	    this->matrix_size = matrix_size;
	}

	virtual ValueType* copy() const {
		return new ValueType(this->builtinType, this->containedType, this->has_vector_size,
		    this->has_matrix_size, this->has_contained_type, this->vector_size, this->matrix_size);
	}
	
	BuiltinType getType() { return this->builtinType; }

    bool hasVectorSize() {
        return this->has_vector_size;
    }
	void setVectorSize(int size) {
	    if (this->builtinType != VectorType && this->builtinType != MatrixType) {
            printf("not a vector type");
            exit(1);
        }

	    this->vector_size = size;
	    this->has_vector_size = true;
	}

    bool hasMatrixSize() {
        return this->has_matrix_size;
    }
	void setMatrixSize(int size) {
	    if (this->builtinType != MatrixType) {
            printf("not a vector type");
            exit(1);
        }

	    this->matrix_size = size;
	    this->has_matrix_size = true;

	}

    bool hasContainedType() {
        return this->has_contained_type;
    }
	void setContainedType(BuiltinType type) {
	    this->containedType = type;
	    this->has_contained_type = true;
	}

	BuiltinType getContainedType() {
	// todo: judge if matrices are vector types
	    if (this->builtinType != VectorType) {
            printf("not a vector type");
            exit(1);
        }

	    if (this->has_contained_type) {
	        return this->containedType;
	    } else {
	        return NullType;
	    }
	}

	int getVectorSize() {
	    if (this->builtinType != VectorType && this->builtinType != MatrixType) {
	        printf("not a vector type");
	        exit(1);
	    }

	    if (this->has_vector_size) {
	        return this->vector_size;
	    } else {
	        return -1;
	    }
	}

	int getMatrixSize() {
	// TODO: Judge if matrices are vectors
	    if (this->builtinType != MatrixType) {
            printf("not a vector type");
            exit(1);
        }

	    if (this->has_matrix_size) {
	        return this->matrix_size;
	    } else {
	        return -1;
	    }
	}


private:
	BuiltinType builtinType;

	// for container objects: matrix and vector
	bool has_contained_type;
	BuiltinType containedType;
	int vector_size;
	bool has_vector_size;
	int matrix_size;
	bool has_matrix_size;

	virtual ~ValueType() {}
	
	typedef Object super;
};
