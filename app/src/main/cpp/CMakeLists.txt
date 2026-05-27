cmake_minimum_required(VERSION 3.22.1)
project("arpspoof")
add_executable(arpspoof arpspoof.c)
set_target_properties(arpspoof PROPERTIES OUTPUT_NAME "arpspoof")
set_target_properties(arpspoof PROPERTIES PREFIX "lib" SUFFIX ".so")

