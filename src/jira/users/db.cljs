(ns jira.users.db
  (:require
    [ajax.core :refer [GET POST]]

    [clojure.string :as str]
    [jira.util :as u :refer [any?]]))
   ; [jira.server :as s]))


;(enable-console-print!)



;;;; -------- INIT -------------

(defonce users (atom {}))
(defonce def-pass "123")

(defonce latest-u-id (atom 0))
(defonce need-registration? (atom 0))

(defonce current-u (atom nil))
(defonce selected-p (atom nil))

(def u-role {:user "user" :admin "admin"})

;;;; -------- GET -----------



(defn next-u-id [] (swap! latest-u-id inc))

(defn count-users [] (count @users))

(defn u-login->id
  [login]
  "Returns user id for given login"
  (first (filter #(= ((@users %) :login) login) (keys @users))))


(defn u-id->login
  [user-id]
  (:login (@users user-id)))


(defn is-admin? [login]
  (= (:role (@users (u-login->id login))) "admin"))


(defn get-u?id
  [user-id]
  (get @users user-id
    {:error "Sorry, no such user"}))


(defn get-u?login
  [login]
  (get @users (u-login->id login)
    {:error "Sorry, no such user"}))


(defn u-exists?
  "Checks if user with login already registered"
  [login]
  (not (nil? (u-login->id login))))
  ;(not (empty? (s/get-u-id login))))


(defn pass-correct?
  [login pass]
  (= (:password (@users (u-login->id login))) pass))



;;;; ---------- CREATE --------

(declare login-u!)
(defn login-u!
  [login pass]
  (cond
    (or (empty? login) (empty? pass))
    {:error "Login and password can't be empty. Please specify!"}
    (not (u-exists? login))
    {:error (str/join ["User with such login: " login " doesn't exist."])}
    (not (pass-correct? login pass))
    {:error "User's pass is wrong"}
    :else
    (do (reset! current-u login)
        (reset! jira.projects.db/selected-p
          (-> @current-u
            jira.projects.db/get-u-projects
            first))
        @current-u)))


(defn logout-u! []
  (do (reset! current-u nil)
      (reset! selected-p nil)
      (reset! need-registration? 0)))


(defn register-u!
  ([login]
   (register-u! login def-pass "user"))
  ([login pass]
   (register-u! login pass "user"))
  ([login pass role]
   (cond
     (u-exists? login)
     {:error (str/join ["User with such login: " login " already registered!
     Try another login name, please."])}
     (or (empty? login) (empty? pass))
     {:error "Login and password can't be empty. Please specify!"}
     :else
     ;(dat/add-user login pass))))
     (let [id (next-u-id)]
       (do
         (if (= login "ira")
           (swap! users assoc id {:id id :login login :password pass :role "admin"})
           (swap! users assoc id {:id id :login login :password pass :role role}))
         (login-u! login pass)
         @current-u)))))



;;;;--------- UPDATE ----------

(defn update-u-login!
  [old-login new-login]
  (cond
    (not (u-exists? old-login))
    {:error (str/join ["User with such login:" old-login "doesn't exist, so nothing to update!"])}
    (u-exists? new-login)
    {:error (str/join ["User with such login:" new-login "already registered!
    Try another login, please!"])}
    :else
    (swap! users assoc-in [(u-login->id old-login) :login] new-login)))



(defn update-u-pass!
  [login old-pass new-pass]
  (cond
    (not (u-exists? login))
    {:error (str/join ["User with such login:" login "doesn't exist, so nothing to update!"])}
    (not (pass-correct? login old-pass))
    {:error (str/join ["User's pass is wrong"])}
    :else
    (swap! users assoc-in [(u-login->id login) :password] new-pass)))


(defn update-u!
  [id new-login new-pass]
  (swap! users assoc id {:id id :login new-login :password new-pass}))




;;;;---------DELETE ------------------------

(defn delete-u!
  [id]
  (if (u-exists? (u-id->login id))
    (swap! users dissoc id)
    {:error (str/join ["User with such login:" (u-id->login id) "doesn't exist, so nothing to delete!"])}))
