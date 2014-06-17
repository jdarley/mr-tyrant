(ns tyranitar.store-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [midje.sweet :refer :all]
            [tyranitar
             [environments :as environments]
             [git :as git]
             [store :refer :all]]))

(def dummy-repo-path "/tmp")

(def dummy-data {:hash "d2d11ce2b4f6f0e96c3d80c1676c1ae4727c4939"
                 :data {:environment.stuff "stuffy"
                        :service.main.thing "wibble"
                        :service.other.thing "wobble"}})

(def update {:service.max.asg 3
             :service.min.asg 1
             :service.preferred.asg 2
             :service.other.thing "changed"})

(def expected-update {:environment.stuff "stuffy"
                      :service.main.thing "wibble"
                      :service.max.asg 3
                      :service.min.asg 1
                      :service.other.thing "changed"
                      :service.preferred.asg 2})

(defn check-poke-deployment-params
  [props template-params]
  (let [name (:name template-params)]
    (= ["Brislabs-8080" "Brislabs-SSH"] (:selectedSecurityGroups props))))

(defn check-prod-deployment-params
  [props template-params]
  (let [name (:name template-params)]
    (= ["internal-8080" "ICM Scanning" "AppGate"] (:selectedSecurityGroups props))))

(defn is-correct-val-for-env
  [env val]
  (if (= "prod" env)
    val
    (not val)))

(defn check-app-props
  [props template-params]
  (and
   (= (:env template-params) (:environment.name props))
   (= (:name template-params) (:service.name props))
   (is-correct-val-for-env (:env template-params) (:service.production props))))

(defn property-values-are-correctly-templated
  [params f]
  (try
    (write-templated-properties (:name params) (:template params) (:env params))
    (let [temp-file (str dummy-repo-path "/" (:template params) ".json")
          props (json/parse-string (slurp temp-file) true)]
      (f props params))
    (finally
      (io/delete-file (str dummy-repo-path "/" (:template params) ".json")))))

(fact "that template values are correctly substituted in prod deployment params"
      (property-values-are-correctly-templated
       {:name "test" :template "deployment-params" :env "prod"}
       check-prod-deployment-params)
      => true
      (provided
       (git/repo-path anything) => dummy-repo-path))

(fact "that template values are correctly substituted in poke deployment params"
      (property-values-are-correctly-templated
       {:name "test" :template "deployment-params" :env "poke"}
       check-poke-deployment-params)
      => true
      (provided
       (git/repo-path anything) => dummy-repo-path))

(fact "that template values are correctly substituted in poke application properties"
      (property-values-are-correctly-templated
       {:name "test" :template "application-properties" :env "poke"}
       check-app-props)
      => true
      (provided
       (git/repo-path anything) => dummy-repo-path))

(fact "that template values are correctly substituted in prod application properties"
      (property-values-are-correctly-templated
       {:name "test" :template "application-properties" :env "prod"}
       check-app-props)
      => true
      (provided
       (git/repo-path anything) => dummy-repo-path))

(fact "that existing properties are updated with correct values"
      (update-properties "dummy" "poke" "dummy-props" update) => anything
      (provided
       (get-data "poke" "dummy" "head" "dummy-props") => dummy-data
       (spit "/tmp/repos/dummy-poke/dummy-props.json" (json/generate-string expected-update {:pretty true})) => anything
       (git/commit-and-push anything anything) => anything))

(fact "that when creating applications we go through each of them"
      (create-application "application") => {:repositories ["repoenv2" "repoenv1"]}
      (provided
       (environments/default-environments) => {:env1 {:name "env1"}
                                               :env2 {:name "env2"}}
       (create-application-env "application" "env1") => "repoenv1"
       (create-application-env "application" "env2") => "repoenv2"))
