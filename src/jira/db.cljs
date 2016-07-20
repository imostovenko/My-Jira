(ns jira.db
  (:require
    [jira.users.db :as udb]
    [jira.projects.db :as pdb]
    [jira.tickets.db :as tdb]

    [ajax.core :refer [GET POST]]
    [jira.util :as ut]))


(enable-console-print!)




;;;; --------DB---------------

(defn collect-db []
  {:users    @udb/users
   :projects @pdb/projects
   :tickets  @tdb/tickets
   :ids      {:l-u-id @udb/latest-u-id
              :l-p-id @pdb/latest-p-id
              :l-t-id @tdb/latest-t-id}})


(defn save-db []
  (POST "/api/db/save"
    {:params        (collect-db)
     :format        :transit
     :handler       #(println "ok" %)
     :error-handler #(println %)}))


(defn init-db
  [db]
  (reset! udb/users (:users db))
  (reset! pdb/projects (:projects db))
  (reset! tdb/tickets (:tickets db))
  (reset! udb/latest-u-id (:l-u-id (:ids db)))
  (reset! pdb/latest-p-id (:l-p-id (:ids db)))
  (reset! tdb/latest-t-id (:l-t-id (:ids db)))
  (add-watch udb/users :watch-u save-db)
  (add-watch pdb/projects :watch-u save-db)
  (add-watch tdb/tickets :watch-u save-db))


(defn spy [x & [pref]]
  (println (pr-str pref x))
  x)


(defn on-db-get
  [data]
  (-> data (spy "one") ut/transit->obj (spy "two") init-db)
  (println "loaded atoms from file"))


(defn sync-GET
  [url on-success]
  (doto (js/XMLHttpRequest.)
    (.open "GET" url false)
    (aset "onload" #(-> % .-target .-responseText on-success))
    (.send)))


(defn get-db []
  (sync-GET "/api/db/get" on-db-get))


(println "before get")
(get-db)
(println "after get")


;--------------------------------
(defn add-new-attr!
  [map new-attr def-value]
  (for [i (keys (deref map))]
    (swap! map assoc-in [i new-attr] def-value)))


;------------ STATUS ------------
(defn status []
  (println "Status:")
  (println (udb/count-users) "users registered")
  (println (pdb/count-projects) "jira.projects created")
  (println (tdb/count-tickets) "tickets")
  (println @udb/current-u "- current user")
  (println @pdb/selected-p "- selected project"))


(defn reset-all []
  (reset! udb/users {})
  (reset! pdb/projects {})
  (reset! tdb/tickets {})
  (reset! udb/latest-u-id 0)
  (reset! pdb/latest-p-id 0)
  (reset! tdb/latest-t-id 0)
  (reset! udb/current-u nil)
  (reset! pdb/selected-p nil)
  (status))


(defn init []
  (udb/register-u! "ira")
  (pdb/create-p! "First" "Some short project description here")
  (tdb/create-t! (pdb/p-title->id "First") "Ticket title" "Decsription")
  (status))

