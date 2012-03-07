(function() {
  window.display_search = function() {
    return $.post("/waiting", {
      "search-string": recipe_title_string
    }, function(result) {
      console.log(result);
      $('#main-content-div').html(result);
      setTimeout(function() {
        return display_search();
      }, 500);
      return setTimeout(function() {
        return window.location.href = recipe_title_string;
      }, 8000);
    });
  };
}).call(this);
