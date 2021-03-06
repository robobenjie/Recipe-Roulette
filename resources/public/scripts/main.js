(function() {
  var refresh;
  window.display_search = function() {};
  refresh = function() {
    var keyword_text;
    keyword_text = $('#main-input').val();
    keyword_text = keyword_text.replace(/[^a-zA-Z\'\s]/, '');
    keyword_text = keyword_text.replace(/\s+/g, '-');
    return window.location.href = keyword_text;
  };
  $(document).ready(function() {
    $('#make-recipe-btn').on("click", refresh);
    $('#random-btn').on("click", function() {
      return window.location.href = random_title;
    });
    $('#main-input').keyup(function(e) {
      if (e.which === 13) {
        return refresh();
      }
    });
    $('#main-input').focus();
    return display_search();
  });
}).call(this);
