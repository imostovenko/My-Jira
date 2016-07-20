(ns jira.text
  (:require
    [jira.components :as comp]
    [rum.core :include-macros true :as rum]))


(defonce lang (atom :EN))

(defn set-lang! [la]
  (reset! lang la))


(declare change-lang)
(rum/defc change-lang
  []
  (let [l {:EN "en"
           :UA "ua"}]
           ;:RU "ru"}]
    [:div.btn-group-vertical.btn-group-xs.col-sm-6
     (for [i (keys l)]
       [:button.btn.btn-default#lang
        {:class (if (= i @lang) "active" "")
         :type "button"
         :value i
         :on-click #(set-lang! i)}
        (i l)])]))


(def copies
  {
   ;----HELLO SECTION-----
   :hello
     {:EN "Hello, "
      :UA "Привіт, "
      :RU "Привет, "}
   :greeting
     {:EN "You can create a new
                  project or go to browse tickets\nof your existing projects!"
      :UA "Створи новий проект, або ж гайда до роботи!"
      :RU "Все - тлен!"}
   :create-new-prj
     {:EN "Create New Project"
      :UA "Створити Проект"
      :RU "Создать Проект"}
   :loged-in
     {:EN "Signed in as "
      :UA "Користувач - "}

   ;----PROJECT CARD-----
   :created-by
     {:EN "Created by "
      :UA "Автор - "
      :RU "Автор - "}
   :view-tickets
     {:EN "View tickets "
      :UA "Проектні задачі "
      :RU "Проектные задачи "}
   :warn-no-prj
     {:EN "You don't have projects yet. Please create your first one."
      :UA "У вас поки що жодного проекту. Створіть свій перший."}


   ;----PROJECT MODAL-----
   :edit-prj
     {:EN "Edit Project - "
      :UA "Редагувати Проект - "
      :RU ""}
   :create-prj
     {:EN "Create NEW Project "
      :UA "Створити Новий Проект "
      :RU ""}
   :title
     {:EN "Title:"
      :UA "Назва:"
      :RU ""}
   :hint-title
     {:EN "project title"
      :UA "назва проекту"}
   :description
     {:EN "Description:"
      :UA "Опис Проекту:"
      :RU ""}
   :hint-description
     {:EN "short project description"
      :UA "короткий опис проекту"}


   ;;----- TICKETS ----
   :tickets
     {:EN " tickets:"
      :UA " завдання:"}
   :hint-search
     {:EN "Search by ticket subj"
      :UA "Пошук за назвою завдання"}
   :filter
     {:EN "Filter  "
      :UA "Фільтр"}
   :new-ticket
     {:EN "New Ticket"
      :UA "Нове Завдання"}
   :warn-no-tickets-yet
     {:EN "No tickets for that project yet."
      :UA "Поки що жодного завдання"}
   :warn-no-tickets-filter
     {:EN "Sorry, no tickets for your selection,
            try change the filter params."
      :UA "Вибач, жодного завдання не знайдено :("}
   :warn-no-tickets-search
     {:EN "Sorry, no tickets for your search,
           try another search params."
      :UA "Вибач, жодного завдання не знайдено :("}
   :create-new-ticket
     {:EN "Create New Ticket"
      :UA "Створити Нове Завдання"}
   :edit-ticket
     {:EN "Edit Ticket - "
      :UA "Змінити Завдання - "}
   :ticket-detail
     {:EN "Ticket Details"
      :UA "Деталі Завдання"}
   :hint-ticket-detail
     {:EN "enter requirements and task details"
      :UA "вимоги до завдання та деталі"}
   :id
     {:EN "ID"
      :UA "ID"}
   :type
     {:EN "Type"
      :UA "Тип"}
   :priority
     {:EN "Priority"
      :UA "Пріоритет"}
   :subject
     {:EN "Subject"
      :UA "Назва Завдання"}
   :hint-subject
     {:EN "task subject"
      :UA "коротка назва завдання"}
   :assignee
     {:EN "Assignee"
      :UA "Виконавець"}
   :creator
     {:EN "Creator"
      :UA "Автор"}
   :status
     {:EN "Status"
      :UA "Статус"}
   :actions
     {:EN "Actions"
      :UA "Дії"}
   :by-type
     {:EN "by Type:"
      :UA "за Типом:"}
   :by-priority
     {:EN "by Priority:"
      :UA "за Пріоритетом:"}
   :by-status
     {:EN "by Status:"
      :UA "за Статусом:"}
   :by-assignee
     {:EN "by Assignee:"
      :UA "за Виконавцем:"}

;;;;=====
   :address
     {:EN "Address:"
      :UA "Адреса"}
   :hint-search-location
     {:EN "enter address of teh venue"
      :UA "вкажіть адресу закладу"}
   :search
     {:EN "Search"
      :UA "Знайти"}




   ;;------ALL MODALs------

   :delete
     {:EN "Delete"
      :UA "Видалити"}
   :cancel
     {:EN "Cancel"
      :UA "Відмінити"}
   :save
     {:EN "Save"
      :UA "Зберегти"}
   :create
     {:EN "Create"
      :UA "Створити"}})






(defn t [text]
  (@lang (text copies)))
  ;(:EN (text copies)))

