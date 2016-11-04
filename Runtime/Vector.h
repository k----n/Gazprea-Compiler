#pragma once

#include "Object.h"
#include "VectorNode.h"

class Value;

/// - Note: T is a subclass of `Object`
template <class T>
class Vector : public Object {
public:
	Vector() : super(TypeVector) {
		this->count = 0;
		this->firstNode = nullptr;
	}
	
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
	
	void set(int index, T* element) {
		if (index < 0) { printf("Index out of bounds\n"); exit(1); }
		if (index >= this->count) { printf("Index out of bounds\n"); exit(1); }
		
		((Object*)element)->retain();
		
		VectorNode* node = this->firstNode;
		for (int i = 0; i < index; ++i) {
			node = node->next;
		}
		node->value->release();
		node->value = element;
	}
	
	T* get(int index) const {
		if (index < 0) { printf("Index out of bounds\n"); exit(1); }
		if (index >= this->count) { printf("Index out of bounds\n"); exit(1); }
		
		VectorNode* node = this->firstNode;
		for (int i = 0; i < index; ++i) {
			node = node->next;
		}
		T* value = (T*)node->value;
		((Object*)value)->retain();
		return value;
	}
	
	Value* getLvalue(int index) const;
	
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
	VectorNode* firstNode;
	int count;
	
	typedef Object super;
};
