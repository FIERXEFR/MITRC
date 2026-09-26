cmake_minimum_required(VERSION 3.22.1)
project("mitrcnative")

add_library(
        mitrcnative
        SHARED
        native-lib.cpp)

find_library(log-lib log)

target_link_libraries(
        mitrcnative
        ${log-lib})
