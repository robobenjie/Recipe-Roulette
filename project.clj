(defproject recipe "1.0.1"
  :description "Site to make up recipes"
  :dependencies  [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive "1.0.0-SNAPSHOT"]
		 [compojure "0.6.4"]
		 [hiccup "0.3.6"]
		 [redis.clients/jedis "1.5.2"]	
                 [ring "0.2.5"]
                 [net.cgrand/moustache "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
		     [lein-ring "0.4.5"]]
  :source-path "src"
  :ring {:handler recipe.routes/app})