#pragma once

#include "Object.h"
#include "VectorNode.h"

/// - Note: T is a subclass of `Object`
template <class T>
class Vector : public Object {
public:
	Vector() : super(TypeVector) {}
		
	virtual Vector* copy() const {
		Vector* copy = new Vector();
		for (int i = 0; i < this->count; ++i) {
			T* value = this->get(i);
			copy->append(value);
			((Object*)value)->release();
		}
		return copy;
	}
	
	void append(T* element) {
		((Object*)element)->retain();
		
		VectorNode* newNode = new VectorNode;
		newNode->next = nullptr;
		newNode->value = element;

		this->count++;

		if (this->firstNode == nullptr) {
			this->firstNode = newNode;
		} else {
			VectorNode* node = this->firstNode;
			while (node->next != nullptr) {
				node = node->next;
			}
			node->next = newNode;
		}
	}
	
	T* get(int index) const {
		if (index < 0) { printf("Index out of bounds\n"); exit(1); }
		if (index >= this->count) { printf("Index out of bounds\n"); exit(1); }
		
		VectorNode* node = this->firstNode;
		for (int i = 0; i < index; ++i) {
			node = node->next;
		}
		T* value = (T*) node->value;
		((Object*)value)->retain();
		return value;
	}
	
	int getCount() const { return this->count; }

	~Vector() {
    		VectorNode* node = this->firstNode;
    		while (node != nullptr) {
    			VectorNode* nextNode = node->next;
    			node->value->release();
    			delete node;
    			node = nextNode;
    		}
    	}

private:
	VectorNode* firstNode = nullptr;
	int count = 0;
	
	typedef Object super;
};
