var ATEditor = (function(ATEditor, model, ATModelFunctions) {

	function MonsterController($scope, $routeParams) {
		$scope.obj = model.monsters.findById($routeParams.id) || {};
		$scope.getExperience = function() {
			return ATModelFunctions.monsterFunctions.getMonsterExperience($scope.obj);
		};
		
		$scope.addCondition = function(list) {
			list.push({magnitude:1, duration:1, chance:100});
		};
		$scope.removeCondition = function(list, cond) {
			var idx = list.indexOf(cond);
			list.splice(idx, 1);
		};
	}
	
	function MonsterTableController($scope, $routeParams) {
		$scope.monsters = model.monsters.items;
		$scope.getExperience = ATModelFunctions.monsterFunctions.getMonsterExperience;
		$scope.edit = function(monster) {
			window.location = "#/" + model.monsters.id + "/edit/" + monster.id;
		};
		
		$scope.iconID = true;
		$scope.id = true;
		$scope.experience = true;
	}
	
	ATEditor.controllers = ATEditor.controllers || {};
	ATEditor.controllers.MonsterController = MonsterController;
	ATEditor.controllers.MonsterTableController = MonsterTableController;

	return ATEditor;
})(ATEditor, ATEditor.model, ATModelFunctions);
