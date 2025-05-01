package com.example.missingpeople.repositor

class RepWebMVD {

    private val urlMVD = "https://поисковая-база.рф/search/sOrder,dt_pub_date/iOrderType,desc/category,1/iPage,";

    fun getUrlMVD():String{
        return urlMVD;
    }
}