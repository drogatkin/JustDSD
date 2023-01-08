# a script example to build Java project 

project =jdsd
"build_directory" = ./lib
source_directory ="src/java"
#source_directory_extra ="diffutils/src"
doc_directory=doc
build_file ="${project}.jar"
 domain ="org"
resources ="${domain}.${project}.resources"
manifest ="manifest.mf"
main_class= "${domain}.justcodecs.dsd.Player"

CUSTOM CP=../tiny-codec/tools/ID3V2/bin/id3v2.jar

target clean {
    dependency {true}
    exec rm  (
        -r,
        ${build_directory}/${domain},
        ${build_directory}/${build_file}
    )
}

target compile:. {
   dependency {
       or {
              newerthan(${source_directory}/.java,${build_directory}/.class)
       }
   }
   {
        display(Compiling Java src ...)
       newerthan(${source_directory}/.java,${build_directory}/.class)
       assign(main src,~~)
       exec javac (
         -d,
         ${build_directory},
        -cp,
         ${build_directory}${~path_separator~}${CUSTOM CP},
         main src
       )     
      if {
         neq(${~~}, 0)
         then {
            panic("Compilation error(s)")
         }
     }
   }
}

target jar {
      dependency {
         anynewer(${build_directory}/${domain}/*,${build_directory}/${build_file})
      }
      dependency {
          target(compile)
      }
     
     {    display(Jarring ${build_file} ...)
          exec jar (
            -cmf, ${manifest},
            ${build_directory}/${build_file},
            -C,
            ${build_directory},
            ${domain}
          )
        if {
         neq(${~~}, 0)
         then {
            panic("Error(s) at jarring")
         }
       }
     }
}

target run :.: {
    dependency {
        target(jar)
    }
    dependency {true}
    {
        ask(Would you like to run dff extractor of ${project}? [N|y] , N)
        assign(answer, ${~~})
        if {
            eq(${answer},N)
            then {
                exec java (
                    -cp,
                     ${build_directory}/${build_file}${~path_separator~}${CUSTOM CP},
                    ${main_class},
                    ~args~
                   )
            } else {
                  exec java (
                    -jar,
                     ${build_directory}/${build_file},
                    ~args~
                   )
            }
        }
   }
}