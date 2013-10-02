(defproject tyranitar "0.19"
  :description "Tyranitar service"
  :url "http://wikis.in.nokia.com/NokiaMusicArchitecture/Tyranitar"

  :dependencies [[compojure "1.1.5" :exclusions [javax.servlet/servlet-api]]
                 [ring-middleware-format "0.3.1"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/jul-to-slf4j "1.7.5"]
                 [ch.qos.logback/logback-classic "1.0.13"]
;                 [com.ovi.common.logging/logback-appender "0.0.45"]
;                 [com.yammer.metrics/metrics-logback "2.2.0"]
                 [com.ovi.common.metrics/metrics-graphite "2.1.21"]
                 [clj-http "0.7.6"]
                 [cheshire "5.2.0"]
                 [clj-time "0.6.0"]
                 [environ "0.4.0"]
                 [nokia/ring-utils "1.0.1"]
                 [metrics-clojure "1.0.1"]
                 [metrics-clojure-ring "1.0.1"]
                 [org.eclipse.jgit "3.0.0.201306101825-r"]]

  :profiles {:dev {:dependencies [[com.github.rest-driver/rest-client-driver "1.1.32"
                                   :exclusions [org.slf4j/slf4j-nop
                                                javax.servlet/servlet-api
                                                org.eclipse.jetty.orbit/javax.servlet]]
                                  [clj-http-fake "0.4.1"]
                                  [junit "4.11"]
                                  [midje "1.5.1"]
                                  [rest-cljer "0.1.7"]]
                   :plugins [[lein-rpm "0.0.4"]
                             [lein-midje "3.0.1"]
                             [jonase/kibit "0.0.8"]]}}

  :plugins [[lein-ring "0.8.6"]
            [lein-environ "0.4.0"]
            [lein-release "1.0.73"]]

  ;; development token values
  :env {:environment-name "Dev"
        :service-name "tyranitar"
        :service-port "8080"
        :service-url "http://localhost:%s/1.x"
        :restdriver-port "8081"
        :service-logging-filethreshold=info
        :service-logging-path=/var/log/tyranitar
        :environment-entertainment-graphite-host "graphite.brislabs.com"
        :environment-entertainment-graphite-port "8080"
        :service-graphite-post-interval "1"
        :service-graphite-post-unit "MINUTES"
        :service-graphite-enabled "ENABLED"
        :service-production "false"

        :service-base-git-repository-url "ssh://snc@source.nokia.com/tyranitar/git/"
        :service-base-git-repository-path "/tmp/repos/"
        :service-snc-api-base-url "https://source.nokia.com/api/v2/"
        :service-snc-api-username "mdaley"
        :service-snc-api-secret "45186ed1acb4a8f9f5d0ff8f700eb1f7"}

  :lein-release {:release-tasks [:clean :uberjar :pom :rpm]
                 :clojars-url "clojars@clojars.brislabs.com:"}

  :ring {:handler tyranitar.web/app
         :main tyranitar.setup
         :port ~(Integer.  (get (System/getenv) "SERVICE_PORT" "8080"))
         :init tyranitar.setup/setup
         :browser-uri "/1.x/status"
         :nrepl {:start? true}}

  :repositories {"internal-clojars"
                 "http://clojars.brislabs.com/repo"
                 "rm.brislabs.com"
                 "http://rm.brislabs.com/nexus/content/groups/all-releases"}

  :uberjar-name "tyranitar.jar"

  :resource-paths ["shared"]

  :rpm {:name "tyranitar"
        :summary "RPM for Tyranitar service"
        :copyright "Nokia 2013"
        :preinstall {:scriptFile "scripts/rpm/preinstall.sh"}
        :postinstall {:scriptFile "scripts/rpm/postinstall.sh"}
        :preremove {:scriptFile "scripts/rpm/preremove.sh"}
        :postremove {:scriptFile "scripts/rpm/postremove.sh"}
        :requires ["jdk >= 2000:1.6.0_31-fcs"]
        :mappings [{:directory "/usr/local/jetty"
                    :filemode "444"
                    :username "jetty"
                    :groupname "jetty"
                    :sources {:source [{:location "target/tyranitar.jar"}]}}
                   {:directory "/usr/local/jetty/bin"
                    :filemode "744"
                    :username "jetty"
                    :groupname "jetty"
                    :sources {:source [{:location "scripts/bin"}]}}
                   {:directory "/usr/local/deployment/tyranitar/bin"
                    :filemode "744"
                    :sources {:source [{:location "scripts/dmt"}]}}
                   {:directory "/etc/rc.d/init.d"
                    :filemode "744"
                    :username "jetty"
                    :groupname "jetty"
                    :sources {:source [{:location "scripts/service/jetty"}]}}]}

  :main tyranitar.setup)
