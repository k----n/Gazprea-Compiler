#pragma once

#include "notAllowedImports.h"

#include "allowedImports.h"
#include "Object.h"
#include "StackNode.h"

/// - Note: T is a subclass of `Object`
template <class T>
class Stack : public Object {
public:
	Stack() : super(TypeStack) {
		this->topNode = nullptr;
	}
		
	virtual Stack<T>* copy() const {
		Stack<T>* copy = new Stack<T>();
		
		StackNode* originalNode = this->topNode;
		StackNode* workingNode = nullptr;
		while (originalNode != nullptr) {
			StackNode* newNode = new StackNode;
			newNode->next = nullptr;
			newNode->value = originalNode->value;
			((Object*)newNode->value)->retain();
			
			if (workingNode == nullptr) {
				copy->topNode = newNode;
				workingNode = newNode;
			} else {
				workingNode->next = newNode;
				workingNode = newNode;
			}
			
			originalNode = originalNode->next;
		}
		return copy;
	}
	
	void push(T* element) {
		((Object*)element)->retain();
		StackNode* newNode = new StackNode;
		newNode->next = this->topNode;
		newNode->value = element;
		this->topNode = newNode;
	}

	T* peek() {
	    if (this->topNode == nullptr) {
	        printf("Attempting to peek empty stack\n"); exit(1); }
	    T* value = (T*) this->topNode->value;
	    return value;
	}

	T* pop() {
		if (this->topNode == nullptr) {
			printf("Attempting to pop empty stack\n"); exit(1); }
		StackNode* node = this->topNode;
		this->topNode = node->next;
		T* value = (T*)node->value;
		delete node;
		return value;
	}
	
	T* popOrNull() {
		if (this->topNode == nullptr) { return nullptr; }
		return this->pop();
	}
	
private:
	StackNode* topNode;
	
	~Stack() {
		StackNode* node = this->topNode;
		while (node != nullptr) {
			StackNode* nextNode = node->next;
			delete node;
			node = nextNode;
		}
		this->topNode = nullptr;
	}
	
	typedef Object super;
};
