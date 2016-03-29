(ns jira.db
  (:require
    [ajax.core :refer [GET POST]]
    [clojure.string :as str]
    [jira.util :as u :refer [any?]]))


(enable-console-print!)

;------INIT-------

(defn error [& args]
  (apply println args)
  :error)


(declare
  u-login->id
  get-u-projects
  p-title->id
  add-t-to-p!
  add-u-to-p!
  status)


(defonce users (atom {}))
(defonce def-pass "123")
(defonce current-u (atom nil))
(defonce latest-u-id (atom 0))
()
(defn next-u-id [] (swap! latest-u-id inc))
(defn count-users [] (count @users))

(defonce projects (atom {}))
(defn empty-p
  "Creates a new prj to be inserted into the collection"
  [u-name p-title descr]
  {:title       p-title
   :description descr
   :author      u-name
   :tickets     #{}
   :users       #{u-name}})
(defonce latest-p-id (atom 0))
(defn next-p-id [] (swap! latest-p-id inc))
(defn count-projects [] (count @projects))

(defonce tickets (atom {}))
(defonce latest-t-id (atom 0))
(defn next-t-id [] (swap! latest-t-id inc))


(def t-type {:0 "TODO" :1 "FIXME"})
(def t-prior {:0 "High" :1 "Medium" :2 "Low"})
(def t-status {:0 "OPEN" :1 "IN PROGRESS" :2 "DONE"})
(defn empty-t
  [p-id type prior status subj descr assignee]
  {:project     p-id
   :type        type
   :prior       prior
   :status      status
   :subj        subj
   :description descr
   :creator     (u-login->id @current-u)
   :assignee    assignee})
(defn count-tickets [] (count @tickets))

(defonce need-registration? (atom 0))

(defonce selected-p
  ;"Selected project ID"
  (atom nil))

(defn select-p [p-id]
  (reset! selected-p p-id))

;------CHECKS ?----------

(defn u-exists?
  "Checks if user with login already registred"
  [login]
  (not (nil? (u-login->id login))))

(defn pass-correct?
  [login pass]
  (= (:password (@users (u-login->id login))) pass))

(defn p-title-unique?
  "Checks if project title already used"
  [p-title]
  (nil? (p-title->id p-title)))


(defn p-exists?
  "Checks if project with such title already exists. projects are an atom."
  ([p-id]
   (p-exists? projects p-id))
  ([projects p-id]
   (some #(= p-id %) (keys (deref projects)))))


(defn t-exists?
  [t-id]
  (contains? @tickets t-id))


;------TRANS --------------

(defn u-login->id
  [login]
  "Returns user id for given login"
  (first (filter #(= ((@users %) :login) login) (keys @users))))

(defn u-id->login
  [user-id]
  ((@users user-id) :login))


(defn p-title->id
  "Returns prj id for given title"
  [prj-title]
  (first (filter #(= ((@projects %) :title) prj-title) (keys @projects))))


;-------CREATE ---------------
(declare login-u!)
(defn register-u!
  ([login]
   (register-u! login def-pass))
  ([login pass]
   (if (u-exists? login)
     (println "User with such login: " login " already registered!
     Try another login name, please.")
     (let [u-id (next-u-id)]
       (swap! users assoc u-id {:login    login
                                :password pass})
       (login-u! login pass)
       @current-u))))


(defn create-p!
  ([p-title]
   (create-p! p-title ""))
  ([p-title descr]
   (create-p! projects p-title descr))
  ([projects p-title descr]
   (let [id (next-p-id)]
     (when (contains? @projects id)
       (println "contains id"))
     (if (p-title-unique? p-title)
       (do
         (swap! projects assoc id (empty-p @current-u p-title descr))
         (select-p id))
       (error "Project " p-title " has been already created!")))))



(defn create-t!
  ([p-id subj descr]
   (create-t! p-id :0 :0 :0 subj descr (u-login->id @current-u)))

  ([p-id type prior status subj descr assignee]
   (if (and (p-exists? p-id)
         (u-exists? (u-id->login assignee))
         (contains? t-type type)
         (contains? t-prior prior)
         (contains? t-status status))
     (let [t-id (next-t-id)]
       (swap! tickets assoc t-id (empty-t p-id type prior status subj descr assignee))
       (add-t-to-p! p-id t-id)
       t-id)
     (error "project or users (" p-id "or" assignee ") doesn't exist! Check is t-type (:0 :1)
     and t-prior (:0 :1 :2) are valid"))))


;-----------GET --------------

(defn get-u?id
  [user-id]
  (get @users user-id "Sorry, no such user"))


(defn get-u?login
  [login]
  (get @users (u-login->id login) "Sorry, no such user"))


(defn get-p
  "Returns single projects details fo the project ID"
  ([p-id]
   (get-p projects p-id))
  ([projects p-id]
   (get (deref projects) p-id "Sorry, no such project")))


(defn get-u-projects
  "Returns all Projects ids accessible by User"
  [u-name]
  (if (u-exists? u-name)
    (for [p-id (keys @projects)
          :let [u-projects (conj p-id)]
          :when (contains? ((@projects p-id) :users) u-name)]
      u-projects)
    (error "User with login" u-name "doesn't exist")))


(defn get-p-title
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :title)
    (error "Project with ID" p-id "doesn't exist")))

(defn get-p-descr
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :description)
    (error "Project with ID" p-id "doesn't exist")))


(defn who-is-author
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :author)
    (error "Project with ID:" p-id "doesn't exist")))


(defn get-t
  [t-id]
  (@tickets t-id))

(defn get-tt-of-p
  "All tickets ids of project"
  [p-id]
  (when (and (p-exists? p-id) (not(nil? @selected-p)))
    ((get-p p-id) :tickets)))
    ; OR (filter #(= ((@tickets %) :project) prj-title) (keys @tickets)) ;which return (1 2 3)
    ;(error "Project with ID:" p-id " doesn't exist.")))


(defn count-p-tickets
  [p-id]
  (if (p-exists? p-id)
    (count (get-tt-of-p p-id))
    {:error "Project with ID: doesn't exisst."}))

;------------------------------------
;--------- FILTER --------------
;-------------------------------------


(defn filter-by-* [tickets key values]
  (if (empty? values)
    tickets
    (u/filter-vals #(contains? (set values) (get % key)) tickets)))

(defn filter-by-users [users tickets]
  (filter-by-* tickets :assignee users))

(defn filter-by-statuses [statuses tickets]
  (filter-by-* tickets :status statuses))

(defn filter-by-types [types tickets]
  (filter-by-* tickets :type types))

(defn filter-by-priors [priors tickets]
  (filter-by-* tickets :prior priors))

(defn search [s-string tickets]
  (if (empty? s-string)
    tickets
    (u/filter-vals #(str/includes? (str/lower-case (get % :subj)) (str/lower-case s-string)) tickets)))


(defn super-filter [settings tickets]
  (->> tickets
    (filter-by-users (:users settings))
    (filter-by-statuses (:statuses settings))
    (filter-by-types (:types settings))
    (filter-by-priors (:priors settings))))



;------------------------------------
;---------UPDATE ------------------
;------------------------------------

(defn login-u!
  [login pass]
  (cond
    (not (u-exists? login))
    {:error (str/join ["User with such login: " login " doesn't exist."])}
    (pass-correct? login pass)
    (do (reset! current-u login)
        (reset! selected-p (-> @current-u
                             get-u-projects
                             first))
        @current-u)

    :else
    {:error "User's pass is wrong"}))


(defn logout-u! []
  (do (reset! current-u nil)
      (reset! selected-p nil)
      (reset! need-registration? 0)))


(defn update-u-login!
  [old-login new-login]
  (cond
    (not (u-exists? old-login))
    (error "User with such login:" old-login "doesn't exist, so nothing to update!")
    (u-exists? new-login)
    (error "User with such login:" new-login "already registered!
    Try another login, please!")
    :else
    (swap! users assoc-in [(u-login->id old-login) :login] new-login)))



(defn add-new-attr!
  [map new-attr def-value]
  (for [i (keys (deref map))]
    (swap! map assoc-in [i new-attr] def-value)))


(defn update-u-pass!
  [login old-pass new-pass]
  (cond
    (not (u-exists? login))
    (error "User with such login:" login "doesn't exist, so nothing to update!")
    (not (pass-correct? login old-pass))
    (error "User's pass is wrong")
    :else
    (swap! users assoc-in [(u-login->id login) :password] new-pass)))


(defn update-p-title!
  [p-id new-title]
  (cond
    (not (p-exists? p-id))
    (error "Project with ID:" p-id "doesn't exist, so nothing to update!")
    (not (p-title-unique? new-title))
    {:error "Project title: " new-title " already used.
    Use unique projects titles please"}
    :else
    (swap! projects assoc-in [p-id :title] new-title)))


(defn update-p-descr!
  [p-id new-descr]
  (if (p-exists? p-id)
    (swap! projects assoc-in [p-id :description] new-descr)
    (error "Project with ID:" p-id "doesn't exist, so nothing to update!")))


(defn update-t!
  [t-id k v]
  (if (t-exists? t-id)
    (swap! tickets update t-id merge {k v})
    (error "Ticket with ID:" t-id "doesn't exist, nothing to update!")))


(defn update-t-subj!
  [t-id subj]
  (update-t! t-id :subj subj))


(defn update-t-descr!
  [t-id descr]
  (update-t! t-id :description descr))


(defn reassign-t!
  [t-id assi]
  (if (u-exists? assi)
    (do (update-t! t-id :assignee (u-login->id assi))
        (add-u-to-p! (:project (get-t 4)) assi))
    (error "User with login:" assi "doesn't exist, so can't reassign!")))

(defn update-t-type!
  [t-id type]
  (if (contains? #{:0 :1} type)
    (update-t! t-id :type type)
    (error "Status" type "doesn't exist, use the following only: open, in progress, done.")))



(defn update-t-status!
  [t-id status]
  (if (contains? #{:0 :1 :2} status)
    (update-t! t-id :status status)
    (error "Status" status "doesn't exist, use the following only: open, in progress, done.")))

(defn update-t-prior!
  [t-id prior]
  (if (contains? #{:0 :1 :2} prior)
    (update-t! t-id :prior prior)
    (error "Status" prior "doesn't exist, use the following only: open, in progress, done.")))


(defn add-t-to-p!
  [p-id t-id]
  (if (and (p-exists? p-id) (t-exists? t-id))
    (swap! projects update-in [p-id :tickets] conj t-id)
    (error "Project with ID:" p-id " or ticket with ID:" t-id "doesn't exist.")))


(defn remove-t-from-p!
  [p-id t-id]
  (if (and (p-exists? p-id) (t-exists? t-id))
    (swap! projects update-in [p-id :tickets] disj t-id)
    (error "Project with ID:" p-id " or ticket with ID:" t-id " doesn't exist,
    so nothing to update!")))


(defn remove-all-t-from-p!
  [p-id]
  (if (p-exists? p-id)
    (swap! projects assoc-in [p-id :tickets] #{})
    (error "Project with ID:" p-id "doesn't exist,
    so nothing to update!")))


(defn add-u-to-p!
  [p-id user-name]
  (if (and (p-exists? p-id) (u-exists? user-name))
    (swap! projects update-in [p-id :users] conj user-name)
    (error "Project with ID:" p-id " or user" user-name " doesn't exist, so nothing to update!")))


(defn remove-u-from-p!
  "Removes User from Project, so Project becomes unaccessible by that User"
  [p-id user-name]
  (if (and (p-exists? p-id) (u-exists? user-name) (not= (who-is-author p-id) user-name))
    (swap! projects update-in [p-id :users] disj user-name)
    (error "Project with ID:" p-id " or user" user-name "doesn't exist, so nothing to update!
            also can't remove author from project.")))


;---------DELETE ------------------------


(defn delete-u!
  [login]
  (if (u-exists? login)
    (swap! users dissoc (u-login->id login))
    (error "User with such login:" login "doesn't exist, so nothing to delete!")))


(defn delete-t-of-p!
  "Deletes all tickets of selected project if any"
  ([p-id]
   (delete-t-of-p! projects tickets p-id))
  ([projects tickets p-id]
   (if (p-exists? projects p-id)
     (swap! tickets #(apply dissoc @tickets (get-tt-of-p p-id)))
     (error "Project with ID:" p-id "doesn't exist, so nothing to delete"))))


(defn delete-p!
  "Deletes a project and its tickets if any"
  ([p-id]
   (delete-p! projects p-id))
  ([projects p-id]
   (if (p-exists? p-id)
     (do (delete-t-of-p! p-id)
         (swap! projects dissoc p-id)
         (reset! selected-p (-> @current-u
                              get-u-projects
                              first)))

     (error "Project with ID:" p-id "doesn't exist, so nothing to delete"))))


(defn delete-t!
  "Delete single ticket from tickets anв from corresponding project"
  [t-id]
  (if (t-exists? t-id)
    (let [p-id ((@tickets t-id) :project)]
      (remove-t-from-p! p-id t-id)
      (swap! tickets dissoc t-id))
    (error "Ticket with ID:" t-id "doesn't exist, so nothing to delete!")))


;----------DATABASE----------


(defn collect-db []
  {:users    @users
   :projects @projects
   :tickets  @tickets
   :ids      {:l-u-id @latest-u-id
              :l-p-id @latest-p-id
              :l-t-id @latest-t-id}})


(defn save-db []
  (POST "/api/db/save"
    {:params        (collect-db)
     :format        :transit
     :handler       #(println "ok" %)
     :error-handler #(println %)}))


(defn init-db
  [db]
  (reset! users (:users db))
  (reset! projects (:projects db))
  (reset! tickets (:tickets db))
  (reset! latest-u-id (:l-u-id (:ids db)))
  (reset! latest-p-id (:l-p-id (:ids db)))
  (reset! latest-t-id (:l-t-id (:ids db)))
  (add-watch users :watch-u save-db)
  (add-watch projects :watch-u save-db)
  (add-watch tickets :watch-u save-db))

(defn spy [x & [pref]]
  (println (pr-str pref x))
  x)

(defn on-db-get
  [data]
  ;(println data)
  (-> data (spy "one") u/transit->obj (spy "two") init-db)
  (println "loaded atoms from file"))


(defn sync-GET
  [url on-success]
  (doto (js/XMLHttpRequest.)
    (.open "GET" url false)
    (aset "onload" #(-> % .-target .-responseText on-success))
    (.send)))


(defn get-db []
  ;(GET "/api/db/get"
  ;  {:handler       on-db-get
  ;   :error-handler #(println "error" %)}))
  (sync-GET "/api/db/get" on-db-get))


(println "before get")
(get-db)
(println "after get")


;------------ STATUS ------------


(defn reset-all []
  (reset! users {})
  (reset! projects {})
  (reset! tickets {})
  (reset! latest-u-id 0)
  (reset! latest-p-id 0)
  (reset! latest-t-id 0)
  (reset! current-u nil)
  (reset! selected-p nil)
  (status))


(defn init []
  (register-u! "ira")
  (create-p! "First" "Some short project description here")
  (create-t! (p-title->id "First") "Ticket title" "Decsription")
  (status))

(defn status []
  (println "Status:")
  (println (count-users) "users registered")
  (println (count-projects) "projects created")
  (println (count-tickets) "tickets")
  (println @current-u "- current user")
  (println @selected-p "- selected project"))