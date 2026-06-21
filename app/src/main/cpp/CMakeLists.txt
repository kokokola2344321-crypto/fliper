cmake_minimum_required(VERSION 3.18.1)
project("native_wifi")

add_library(
    native_wifi
    SHARED
    native_wifi.c
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native_wifi
    ${log-lib}
)

target_include_directories(
    native_wifi
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
