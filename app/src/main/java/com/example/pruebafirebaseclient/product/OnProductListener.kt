package com.example.pruebafirebaseclient.product

import com.example.pruebafirebaseclient.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
    fun onLongClick(product: Product)
}