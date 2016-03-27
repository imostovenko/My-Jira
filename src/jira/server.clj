(ns jira.server
  (:use [compojure.core :only [defroutes GET POST]])
  (:require
    [compojure.route    :as route]
    [jira.storage  :as storage]
    [org.httpkit.server :as httpkit]
    [ring.util.response :as response])

  (:gen-class))


(defn req->body [req]
  (-> req :body slurp))

(defroutes app
  (GET "/" []
    (response/resource-response "public/index.html"))

  (POST "/api/db/save" [:as req]
    (let [db (req->body req)]
      (storage/save-db db)
      (println "saved")
      {:status  200}))

  (GET "/api/db/get" [:as req]
    {:status  200
     :body (storage/get-db)})

  (route/resources "/" {:root "public"}))


(defn -main [& {:as args}]
  (let [port (or (get args "--port") "8080")]
    (println "Starting server at port" port)
    (httpkit/run-server #'app {:port (Long/parseLong port)})))
